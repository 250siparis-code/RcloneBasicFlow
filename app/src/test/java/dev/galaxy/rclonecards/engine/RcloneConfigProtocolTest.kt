package dev.galaxy.rclonecards.engine

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RcloneConfigProtocolTest {
    @Test fun firstClientIdQuestionStaysOpen() {
        val step = RcloneConfigWizard.parseStep(JSONObject("""{"State":"*all-set,0,false","Error":"","Option":{"Name":"client_id","Default":"","Type":"string"}}"""))
        assertFalse(step.done)
        assertEquals("client_id", step.question!!.name)
        assertEquals("", step.question!!.defaultValue)
    }
    @Test fun clientSecretIsAlwaysMasked() {
        val step = RcloneConfigWizard.parseStep(JSONObject("""{"State":"next","Option":{"Name":"client_secret","IsPassword":false}}"""))
        assertTrue(step.question!!.isPassword)
    }
    @Test fun booleanChoiceUsesRcloneValues() {
        val step = RcloneConfigWizard.parseStep(JSONObject("""{"State":"*oauth-islocal","Option":{"Name":"config_is_local","Default":true,"Examples":[{"Value":true,"Help":"Yes"},{"Value":false,"Help":"No"}],"Exclusive":true}}"""))
        assertEquals("true", step.question!!.defaultValue)
        assertEquals(listOf("true", "false"), step.question!!.examples.map { it.value })
    }
    @Test fun completionDoesNotClaimLiveVerification() {
        val step = RcloneConfigWizard.parseStep(JSONObject("""{"State":"","Option":null,"Error":""}"""))
        assertTrue(step.done)
        assertFalse(step.verified)
    }
    @Test(expected = IllegalArgumentException::class)
    fun errorCannotBeTreatedAsCompletion() {
        RcloneConfigWizard.parseStep(JSONObject("""{"State":"","Option":null,"Error":"OAuth denied"}"""))
    }
    @Test(expected = IllegalArgumentException::class)
    fun missingStateCannotCloseTheDialog() {
        RcloneConfigWizard.parseStep(JSONObject("""{"Option":{"Name":"client_id"}}"""))
    }
    @Test fun invalidAnswerRemainsAQuestion() {
        val step = RcloneConfigWizard.parseStep(JSONObject("""{"State":"scope","Error":"invalid scope","Option":{"Name":"scope"}}"""))
        assertFalse(step.done)
        assertEquals("invalid scope", step.question!!.error)
    }
    @Test fun logBracesDoNotReplaceResponse() {
        val response = RcloneConfigWizard.parseJsonObject("log {invalid}\n{\"State\":\"next\",\"Option\":{\"Name\":\"client_secret\",\"Help\":\"quoted { text }\"}}\nNOTICE end")!!
        assertEquals("client_secret", RcloneConfigWizard.parseStep(response).question!!.name)
    }
}
