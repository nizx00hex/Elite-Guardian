package com.eliteguardian.app.vpn

import com.eliteguardian.app.domain.filtering.DomainMatcher
import com.eliteguardian.app.domain.filtering.SafeSearchRules
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

class DnsPacketHandlerTest {

    private lateinit var matcher: DomainMatcher
    private lateinit var handler: DnsPacketHandler

    @Before
    fun setUp() {
        matcher = DomainMatcher()
        matcher.setBlocklist(listOf("adultsite.com"))
        handler = DnsPacketHandler(matcher, isSafeSearchEnabled = true)
    }

    private fun createRawDnsQuery(id: Int, domain: String, qtype: Int = 1): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        dos.writeShort(id)
        dos.writeShort(0x0100) // Standard query, RD=1
        dos.writeShort(1) // QDCOUNT = 1
        dos.writeShort(0)
        dos.writeShort(0)
        dos.writeShort(0)

        domain.split('.').forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            dos.writeByte(bytes.size)
            dos.write(bytes)
        }
        dos.writeByte(0)
        dos.writeShort(qtype)
        dos.writeShort(1) // IN
        dos.flush()
        return bos.toByteArray()
    }

    @Test
    fun testParseDnsQuery() {
        val queryBytes = createRawDnsQuery(0x1234, "example.com")
        val parsed = handler.parseDnsQuery(queryBytes, queryBytes.size)

        assertNotNull(parsed)
        assertEquals(0x1234, parsed!!.id)
        assertEquals("example.com", parsed.qname)
        assertEquals(1, parsed.qtype)
    }

    @Test
    fun testEvaluateDecision() {
        assertEquals(DnsPacketHandler.Decision.BLOCK, handler.evaluate("adultsite.com"))
        assertEquals(DnsPacketHandler.Decision.BLOCK, handler.evaluate("sub.adultsite.com"))
        assertEquals(DnsPacketHandler.Decision.SAFESEARCH, handler.evaluate("google.com"))
        assertEquals(DnsPacketHandler.Decision.FORWARD, handler.evaluate("wikipedia.org"))
    }

    @Test
    fun testSynthesizeDnsResponse() {
        val queryBytes = createRawDnsQuery(0xABCD, "adultsite.com")
        val query = handler.parseDnsQuery(queryBytes, queryBytes.size)!!
        val sinkholeIp = byteArrayOf(0, 0, 0, 0)

        val response = handler.synthesizeDnsResponse(query, sinkholeIp)
        assertNotNull(response)

        val buf = ByteBuffer.wrap(response)
        val id = buf.short.toInt() and 0xFFFF
        val flags = buf.short.toInt() and 0xFFFF
        val qdCount = buf.short.toInt() and 0xFFFF
        val anCount = buf.short.toInt() and 0xFFFF

        assertEquals(0xABCD, id)
        assertEquals(0x8180, flags) // Response flag set
        assertEquals(1, qdCount)
        assertEquals(1, anCount)
    }

    @Test
    fun testBuildIpv4UdpPacket() {
        val srcIp = byteArrayOf(10, 0, 0, 2)
        val dstIp = byteArrayOf(10, 0, 0, 1)
        val payload = byteArrayOf(1, 2, 3, 4, 5, 6)

        val packet = handler.buildIpv4UdpPacket(srcIp, dstIp, 53, 54321, payload)

        assertEquals(20 + 8 + payload.size, packet.size)
        // Verify IPv4 Version & IHL
        assertEquals(0x45.toByte(), packet[0])
        // Verify UDP Protocol
        assertEquals(17.toByte(), packet[9])
    }
}

