package com.eliteguardian.app.vpn

import com.eliteguardian.app.domain.filtering.DomainMatcher
import com.eliteguardian.app.domain.filtering.SafeSearchRules
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class DnsPacketHandler(
    private val matcher: DomainMatcher,
    private val isSafeSearchEnabled: Boolean = true,
    private val upstreamResolver: InetAddress = InetAddress.getByName("1.1.1.3") // Cloudflare Family (adult content blocked by default)
) {

    data class DnsQuery(
        val id: Int,
        val qname: String,
        val qtype: Int,
        val rawPayload: ByteArray
    )

    enum class Decision {
        BLOCK,
        SAFESEARCH,
        FORWARD
    }

    /**
     * Parses a raw DNS query payload from UDP packet.
     */
    fun parseDnsQuery(payload: ByteArray, length: Int): DnsQuery? {
        if (length < 12) return null
        val buffer = ByteBuffer.wrap(payload, 0, length)
        val id = buffer.short.toInt() and 0xFFFF
        val flags = buffer.short.toInt() and 0xFFFF
        val isQuery = (flags and 0x8000) == 0
        if (!isQuery) return null

        val qdCount = buffer.short.toInt() and 0xFFFF
        if (qdCount < 1) return null

        // Skip ANCOUNT, NSCOUNT, ARCOUNT
        buffer.short
        buffer.short
        buffer.short

        // Parse QNAME
        val domainBuilder = StringBuilder()
        while (buffer.hasRemaining()) {
            val labelLen = buffer.get().toInt() and 0xFF
            if (labelLen == 0) break
            if (labelLen > 63 || buffer.remaining() < labelLen) return null

            if (domainBuilder.isNotEmpty()) domainBuilder.append('.')
            val labelBytes = ByteArray(labelLen)
            buffer.get(labelBytes)
            domainBuilder.append(String(labelBytes, Charsets.US_ASCII))
        }

        if (!buffer.hasRemaining() || buffer.remaining() < 4) return null
        val qtype = buffer.short.toInt() and 0xFFFF
        val qclass = buffer.short.toInt() and 0xFFFF

        return DnsQuery(
            id = id,
            qname = domainBuilder.toString(),
            qtype = qtype,
            rawPayload = payload.copyOf(length)
        )
    }

    /**
     * Evaluates domain decision.
     */
    fun evaluate(qname: String): Decision {
        if (matcher.isBlocked(qname)) {
            return Decision.BLOCK
        }
        if (isSafeSearchEnabled && SafeSearchRules.getSafeSearchIp(qname) != null) {
            return Decision.SAFESEARCH
        }
        return Decision.FORWARD
    }

    /**
     * Synthesizes a DNS response for Sinkhole (0.0.0.0 for A, :: for AAAA) or SafeSearch IP.
     */
    fun synthesizeDnsResponse(query: DnsQuery, redirectIp: ByteArray?): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        // 1. Transaction ID
        dos.writeShort(query.id)

        // 2. Flags: Response, Opcode 0, Authoritative, Recursion Desired, Recursion Available, RCODE 0
        // QR(1) | AA(1) | RD(1) | RA(1) = 0x8180
        val flags = 0x8180
        dos.writeShort(flags)

        // 3. QDCOUNT = 1
        dos.writeShort(1)

        // 4. ANCOUNT = if redirectIp != null then 1 else 0
        dos.writeShort(if (redirectIp != null) 1 else 0)

        // 5. NSCOUNT = 0, ARCOUNT = 0
        dos.writeShort(0)
        dos.writeShort(0)

        // 6. Question Section
        // Re-encode QNAME labels
        val labels = query.qname.split('.')
        for (label in labels) {
            val labelBytes = label.toByteArray(Charsets.US_ASCII)
            dos.writeByte(labelBytes.size)
            dos.write(labelBytes)
        }
        dos.writeByte(0) // Terminating null

        dos.writeShort(query.qtype) // QTYPE
        dos.writeShort(1) // QCLASS IN

        // 7. Answer Section (if returning an IP)
        if (redirectIp != null) {
            // Pointer to QNAME at offset 12: 0xC00C
            dos.writeShort(0xC00C)
            // TYPE
            if (redirectIp.size == 4) {
                dos.writeShort(1) // A
            } else {
                dos.writeShort(28) // AAAA
            }
            // CLASS IN
            dos.writeShort(1)
            // TTL (e.g. 300 seconds)
            dos.writeInt(300)
            // RDLENGTH
            dos.writeShort(redirectIp.size)
            // RDATA
            dos.write(redirectIp)
        }

        dos.flush()
        return bos.toByteArray()
    }

    /**
     * Forwards an allowed query to upstream DNS server over protected socket.
     */
    fun forwardUpstream(socket: DatagramSocket, query: DnsQuery): ByteArray? {
        return try {
            val sendPacket = DatagramPacket(
                query.rawPayload,
                query.rawPayload.size,
                upstreamResolver,
                53
            )
            socket.send(sendPacket)

            val receiveBuf = ByteArray(1500)
            val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
            socket.soTimeout = 2500 // 2.5 second timeout
            socket.receive(receivePacket)

            receivePacket.data.copyOf(receivePacket.length)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds a complete IPv4 + UDP packet containing the DNS payload.
     */
    fun buildIpv4UdpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalIpLength = 20 + 8 + payload.size
        val packet = ByteArray(totalIpLength)
        val buf = ByteBuffer.wrap(packet)

        // IPv4 Header
        buf.put(0x45.toByte()) // Version 4, IHL 5
        buf.put(0x00.toByte()) // DSCP/ECN
        buf.putShort(totalIpLength.toShort()) // Total Length
        buf.putShort(0x0000.toShort()) // Identification
        buf.putShort(0x4000.toShort()) // Flags (Don't Fragment)
        buf.put(64.toByte()) // TTL
        buf.put(17.toByte()) // Protocol UDP (17)
        val ipChecksumPos = buf.position()
        buf.putShort(0.toShort()) // IP Checksum placeholder
        buf.put(srcIp) // Source IP
        buf.put(dstIp) // Dest IP

        // Compute IPv4 header checksum
        val ipChecksum = computeIpChecksum(packet, 0, 20)
        packet[ipChecksumPos] = (ipChecksum.toInt() shr 8).toByte()
        packet[ipChecksumPos + 1] = (ipChecksum.toInt() and 0xFF).toByte()

        // UDP Header
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        val udpLength = 8 + payload.size
        buf.putShort(udpLength.toShort())
        buf.putShort(0.toShort()) // Optional UDP checksum set to 0

        // DNS Payload
        buf.put(payload)

        return packet
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Short {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv() and 0xFFFF).toShort()
    }
}

