package com.slackcat.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandParserTest {
    @Test
    fun `extractCommand should return the correct command when message starts with question mark`() {
        val result = CommandParser.extractCommand("?ping --help")
        assertEquals("ping", result)
    }

    @Test
    fun `extractCommand should return command when message has space between question mark and command`() {
        val result = CommandParser.extractCommand("? ping --help")
        assertEquals("ping", result)
    }

    @Test
    fun `extractCommand should return null when message does not contain a question mark`() {
        val result = CommandParser.extractCommand("ping --help")
        assertEquals(null, result)
    }

    @Test
    fun `validateCommandMessage should return true when message starts with question mark`() {
        val result = CommandParser.validateCommandMessage("?ping --help")
        assertTrue(result)
    }

    @Test
    fun `validateCommandMessage should return false when message does not start with question mark`() {
        val result = CommandParser.validateCommandMessage("ping --help")
        assertFalse(result)
    }

    @Test
    fun `validateCommandMessage should return false for an empty message`() {
        val result = CommandParser.validateCommandMessage("")
        assertFalse(result)
    }

    @Test
    fun `validateCommandMessage should return true for message starting with question mark and spaces after`() {
        val result = CommandParser.validateCommandMessage("? ping --help")
        assertTrue(result)
    }

    @Test
    fun `extractArguments should return a list of arguments when they start with --`() {
        val result = CommandParser.extractArguments("?ping --help --verbose --output")
        assertEquals(listOf("--help", "--verbose", "--output"), result)
    }

    @Test
    fun `extractArguments should return an empty list when there are no arguments`() {
        val result = CommandParser.extractArguments("?ping")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `extractArguments should return only valid arguments when some text does not start with --`() {
        val result = CommandParser.extractArguments("?ping --help someText --verbose")
        assertEquals(listOf("--help", "--verbose"), result)
    }

    @Test
    fun `extractUserText should return the text after the command`() {
        val result = CommandParser.extractUserText("?ping --help --verbose This is user text.")
        assertEquals("--help --verbose This is user text.", result)
    }

    @Test
    fun `extractUserText should return the entire message if no command is present`() {
        val result = CommandParser.extractUserText("This is user text without a command.")
        assertEquals("This is user text without a command.", result)
    }

    @Test
    fun `extractUserText should return empty string if only the command is present`() {
        val result = CommandParser.extractUserText("?ping")
        assertEquals("", result)
    }

    @Test
    fun `extractUserText should handle message with spaces after command`() {
        val result = CommandParser.extractUserText("?ping    This is user text.")
        assertEquals("This is user text.", result)
    }
}
