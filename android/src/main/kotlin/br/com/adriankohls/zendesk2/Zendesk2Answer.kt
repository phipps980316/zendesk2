package br.com.adriankohls.zendesk2

import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import zendesk.answerbot.AnswerBot
import zendesk.answerbot.DeflectionArticle
import zendesk.answerbot.DeflectionResponse
import zendesk.answerbot.RejectionReason
import zendesk.core.AnonymousIdentity
import zendesk.core.Identity
import zendesk.core.JwtIdentity
import zendesk.core.Zendesk
import zendesk.support.Guide
import zendesk.support.Support

class Zendesk2Answer(private val plugin: Zendesk2Plugin, private val channel: MethodChannel) {
    val provider = AnswerBot.INSTANCE.provider();

    fun initialize(call: MethodCall) {
        val appId = call.argument<String>("appId")!!
        val clientId = call.argument<String>("clientId")!!
        val zendeskUrl = call.argument<String>("zendeskUrl")!!

        val token = call.argument<String>("token")
        val name = call.argument<String>("name")
        val email = call.argument<String>("email")

        if (plugin.activity != null) {
            val zendesk = Zendesk.INSTANCE
            val answer = AnswerBot.INSTANCE
            val guide = Guide.INSTANCE

            zendesk.init(plugin.activity!!, zendeskUrl, appId, clientId)

            val identity: Identity =
                    if (token != null) {
                        JwtIdentity(token)
                    } else {
                        val builder = AnonymousIdentity.Builder()
                        if (name != null) builder.withNameIdentifier(name)
                        if (email != null) builder.withEmailIdentifier(email)
                        builder.build()
                    }
            Zendesk.INSTANCE.setIdentity(identity)

            guide.init(zendesk)
            answer.init(zendesk, guide)
        } else {
            print("Plugin Context is NULL!")
        }

    }

    fun deflectionQuery(call: MethodCall) {
        val query = call.argument<String>("query")!!

        provider?.getDeflectionForQuery(
                query,
                object : ZendeskCallback<DeflectionResponse>() {
                    override fun onSuccess(success: DeflectionResponse?) {
                        val deflection = success?.deflection
                        val articles = success?.deflectionArticles ?: listOf<DeflectionArticle>()

                        val deflectionId = deflection?.id
                        val interactionAccessToken = success?.interactionAccessToken

                        val dictionary = mutableMapOf<String, Any?>()

                        dictionary["interactionAccessToken"] = interactionAccessToken
                        dictionary["deflectionId"] = deflectionId
                        dictionary["description"] = null

                        val mArticles = mutableListOf<Map<String, Any?>>()
                        for (article in articles) {
                            val mArticle = mutableMapOf<String, Any?>()

                            mArticle["deflectionArticleId"] = article.id
                            mArticle["brandId"] = article.brandId
                            mArticle["articleId"] = article.articleId
                            mArticle["body"] = null
                            mArticle["htmlURL"] = article.htmlUrl
                            mArticle["labels"] = article.labelNames
                            mArticle["locale"] = article.locale
                            mArticle["score"] = article.score
                            mArticle["snippet"] = article.snippet
                            mArticle["title"] = article.title
                            mArticle["url"] = article.url

                            mArticles.add(mArticle)
                        }

                        dictionary["articles"] = mArticles

                        channel.invokeMethod("sendAnswerProviderModel", dictionary)
                    }

                    override fun onError(error: ErrorResponse?) {
                        print(error.toString())
                    }
                })
    }

    fun resolveArticleDeflection(call: MethodCall) {
        val deflectionId = call.argument<Long>("deflectionId")!!
        val articleId = call.argument<Long>("articleId")!!
        val interactionAccessToken = call.argument<String>("interactionAccessToken")!!

        provider?.resolveWithArticle(
                deflectionId,
                articleId,
                interactionAccessToken,
                object : ZendeskCallback<Void>() {
                    override fun onSuccess(void: Void?) {
                        val dictionary = mutableMapOf<String, Any?>()
                        dictionary["success"] = true
                        channel.invokeMethod("sendResolveArticleDeflection", dictionary)
                    }

                    override fun onError(response: ErrorResponse?) {
                        val dictionary = mutableMapOf<String, Any?>()
                        dictionary["success"] = false
                        channel.invokeMethod("sendResolveArticleDeflection", dictionary)
                    }

                })
    }

    fun rejectArticleDeflection(call: MethodCall) {
        val deflectionId = call.argument<Long>("deflectionId")!!
        val articleId = call.argument<Long>("articleId")!!
        val interactionAccessToken = call.argument<String>("interactionAccessToken")!!
        val reason = call.argument<String>("reason")!!

        val mReason = when (reason) {
            "NOT_RELATED" -> RejectionReason.NOT_RELATED
            "RELATED_BUT_DIDNT_ANSWER" -> RejectionReason.RELATED_DIDNT_ANSWER
            else -> RejectionReason.UNKNOWN
        }

        provider?.rejectWithArticle(
                deflectionId,
                articleId,
                interactionAccessToken,
                mReason,
                object : ZendeskCallback<Void>() {
                    override fun onSuccess(void: Void?) {
                        val dictionary = mutableMapOf<String, Any?>()
                        dictionary["success"] = true
                        channel.invokeMethod("sendRejectArticleDeflection", dictionary)
                    }

                    override fun onError(response: ErrorResponse?) {
                        val dictionary = mutableMapOf<String, Any?>()
                        dictionary["success"] = false
                        channel.invokeMethod("sendRejectArticleDeflection", dictionary)
                    }
                })
    }
}