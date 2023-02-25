package raw.http

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import io.circe.Json
import io.circe._
import io.circe.generic.semiauto.deriveCodec
import io.circe.literal._
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Uri
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.jsonDecoder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.netty.client.NettyClientBuilder
import org.mockito.ArgumentMatchersSugar
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import raw.HttpClient
import raw.http.HttpClientSpec.TestJson
import raw.model.RawError.RawBadRequestError
import raw.model.RawError.RawDeserializationError
import raw.model.RawError.RawHttpError
import raw.model.RedditRequest.RedditRequest
import raw.model.RedditRequest.RedditRequest.redditRequestCodec

class HttpClientSpec
    extends AnyFunSuite
    with Matchers
    with ArgumentMatchersSugar
    with MockFactory
    with BeforeAndAfterAll {

  val testport = 9995
  val server = new WireMockServer(testport)
  server.start()
  WireMock.configureFor(testport)

  val baseUri = Uri.unsafeFromString(s"http://localhost:$testport")
  val endpoint = "/json"

  override def afterAll() = {
    server.shutdown()
  }

  test("GET: should return a success response with valid Json") {
    val testJson = json"""{ "hello" : "world" }"""
    stubFor(
      WireMock
        .get(endpoint)
        .willReturn(okJson(testJson.toString))
    )
    val resp = NettyClientBuilder[IO].resource
      .use { client =>
        val someClient = HttpClient(client)
        someClient.get[Json](
          uri"http://localhost:9995/json",
          Headers(Header("Some-header", "other-value"))
        )
      }
      .unsafeRunSync()
      .getOrElse(fail())

    resp shouldBe testJson
  }

  test("GET: should return a bad Request response with no json error") {
    stubFor(
      WireMock
        .get(endpoint)
        .willReturn(aResponse().withStatus(400))
    )
    val resp = NettyClientBuilder[IO].resource
      .use { client =>
        val someClient = HttpClient(client)
        someClient.get[Json](
          uri"http://localhost:9995/json",
          Headers(Header("Some-header", "other-value"))
        )
      }
      .unsafeRunSync()
      .left
      .getOrElse(fail())

    resp shouldBe a[RawBadRequestError]
  }

  test(
    "GET: should return a HTTP response for any other uncaught http errors"
  ) {
    stubFor(
      WireMock
        .get(endpoint)
        .willReturn(aResponse().withStatus(500))
    )
    val resp = NettyClientBuilder[IO]
      .withIdleTimeout(10.seconds)
      .resource
      .use { client =>
        val someClient = HttpClient(client)
        someClient
          .get[Json](
            uri"http://localhost:9995/json",
            Headers(Header("Some-header", "other-value"))
          )
      }
      .unsafeRunSync()
      .left
      .getOrElse(fail())

    resp shouldBe a[RawHttpError]
  }

  test(
    "GET: should deserialization error"
  ) {
    stubFor(
      WireMock
        .get(endpoint)
        .willReturn(okJson(json"""{"rich": 1}""".toString))
    )
    val resp = NettyClientBuilder[IO]
      .withIdleTimeout(10.seconds)
      .resource
      .use { client =>
        val someClient = HttpClient(client)
        someClient
          .get[TestJson](
            uri"http://localhost:9995/json",
            Headers(Header("Some-header", "other-value"))
          )
      }
      .unsafeRunSync()
      .left
      .getOrElse(fail())

    resp shouldBe a[RawDeserializationError]
  }

  test("POST: should return valid response for valid request") {
    val validJson = json"""{
      "work": "valid"
    }"""
    stubFor(
      WireMock
        .post(endpoint)
        .willReturn(okJson(validJson.toString))
    )
    val resp = NettyClientBuilder[IO].resource
      .use { client =>
        val someClient = HttpClient(client)
        someClient
          .post[Json, RedditRequest](
            uri"http://localhost:9995/json",
            RedditRequest("hello"),
            Headers(Header("Some-header", "other-value"))
          )
      }
      .unsafeRunSync()
      .getOrElse(fail())

    resp shouldBe validJson
  }

  test("POST: should return badrequest for invalid request") {
    stubFor(
      WireMock
        .post(endpoint)
        .willReturn(aResponse().withStatus(400))
    )
    val resp = NettyClientBuilder[IO].resource
      .use { client =>
        val someClient = HttpClient(client)
        someClient
          .post[Json, RedditRequest](
            uri"http://localhost:9995/json",
            RedditRequest("hello"),
            Headers(Header("Some-header", "other-value"))
          )
      }
      .unsafeRunSync()
      .left
      .getOrElse(fail())

    resp shouldBe a[RawBadRequestError]
  }

  test("POST: should return HttpError for unexpected error") {
    stubFor(
      WireMock
        .post(endpoint)
        .willReturn(aResponse().withStatus(500))
    )
    val resp = NettyClientBuilder[IO].resource
      .use { client =>
        val someClient = HttpClient(client)
        someClient
          .post[Json, RedditRequest](
            uri"http://localhost:9995/json",
            RedditRequest("hello"),
            Headers(Header("Some-header", "other-value"))
          )
      }
      .unsafeRunSync()
      .left
      .getOrElse(fail())

    resp shouldBe a[RawHttpError]
  }

  test(
    "POST: should return deserialization error"
  ) {
    stubFor(
      WireMock
        .post(endpoint)
        .willReturn(okJson(json"""{"rich": 1}""".toString))
    )
    val resp = NettyClientBuilder[IO]
      .withIdleTimeout(10.seconds)
      .resource
      .use { client =>
        val someClient = HttpClient(client)
        someClient
          .post[TestJson, RedditRequest](
            uri"http://localhost:9995/json",
            RedditRequest("hello"),
            Headers(Header("Some-header", "other-value"))
          )
      }
      .unsafeRunSync()
      .left
      .getOrElse(fail())

    resp shouldBe a[RawDeserializationError]
  }
}

object HttpClientSpec {
  final case class TestJson(work: String)
  object TestJson {
    implicit val userDD: Codec[TestJson] = deriveCodec
  }
}
