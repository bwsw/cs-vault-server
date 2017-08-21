package com.bwsw.cloudstack.vault.server.vault.util

import com.bettercloud.vault.VaultException
import com.bettercloud.vault.rest.{Rest, RestResponse}
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 21.08.17.
  */
object VaultRest {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createDeleteRequest(rootToken: String,
                          path: String,
                          data: String,
                          expectedResponseStatus: Int,
                          requestDescription: String):() => RestResponse = {
    createRequest(createRest(rootToken, path, data).delete, expectedResponseStatus, requestDescription)
  }

  def createPutRequest(rootToken: String,
                       path: String,
                       data: String,
                       expectedResponseStatus: Int,
                       requestDescription: String):() => RestResponse = {
    createRequest(createRest(rootToken, path, data).put, expectedResponseStatus, requestDescription)
  }

  def createPostRequest(rootToken: String,
                        path: String,
                        data: String,
                        expectedResponseStatus: Int,
                        requestDescription: String):() => RestResponse = {
    createRequest(createRest(rootToken, path, data).post, expectedResponseStatus, requestDescription)
  }

  def createGetRequest(rootToken: String,
                       path: String,
                       data: String,
                       expectedResponseStatus: Int,
                       requestDescription: String):() => RestResponse = {
    createRequest(createRest(rootToken, path, data).get, expectedResponseStatus, requestDescription)
  }

  private def createRest(rootToken: String, path: String, data: String): Rest = {
    new Rest()
      .url(s"$path")
      .header("X-Vault-Token", rootToken)
      .body(data.getBytes("UTF-8"))
  }

  private def createRequest(request: () => RestResponse,
                            expectedResponseStatus: Int,
                            requestDescription: String)(): RestResponse = {
    logger.debug(s"Request was executed for: $requestDescription")
    val response = request()

    if (response.getStatus != expectedResponseStatus) {
      throw new VaultException(s"Response status: ${response.getStatus} is not expected")
    }
    response
  }
}
