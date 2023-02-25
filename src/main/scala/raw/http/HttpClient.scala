package raw

import cats.MonadThrow
import cats.implicits._
import io.circe.Encoder
import org.http4s.Status
import org.http4s.Uri
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.client.Client
import org.http4s.client.UnexpectedStatus
import raw.model.RawError
import raw.model.RawError.RawBadRequestError
import raw.model.RawError.RawDeserializationError
import raw.model.RawError.RawHttpError
import raw.model.RawError.RawTimeoutError

trait HttpClient[F[_]] {

  def get[A](uri: Uri, headers: Headers)(implicit
      entityDecoder: EntityDecoder[F, A],
      entityDecoders: EntityDecoder[F, String]
  ): F[Either[RawError, A]]

  def post[A, B: Encoder](uri: Uri, requestBody: B, headers: Headers)(implicit
      entityDecoder: EntityDecoder[F, B],
      entityDecoders: EntityDecoder[F, A]
  ): F[Either[RawError, A]]

}

object HttpClient {

  def apply[F[_]: MonadThrow](client: Client[F]): HttpClientImpl[F] = {
    new HttpClientImpl[F](client)
  }

  class HttpClientImpl[F[_]: MonadThrow](client: Client[F])
      extends HttpClient[F] {

    override def get[A](uri: Uri, headers: Headers)(implicit
        entityDecoder: EntityDecoder[F, A],
        entityDecoders: EntityDecoder[F, String]
    ): F[Either[RawError, A]] = {
      client
        .expect[A](getConstructor(uri, headers))
        .attempt
        .flatMap { response =>
          response
            .leftMap {
              case err: UnexpectedStatus =>
                handleErrors(err)
              case InvalidMessageBodyFailure(details, cause) =>
                RawDeserializationError(details, cause)
            }
            .pure[F]
        }
    }

    override def post[A, B: Encoder](uri: Uri, request: B, headers: Headers)(
        implicit
        entityDecoder: EntityDecoder[F, B],
        entityDecoders: EntityDecoder[F, A]
    ): F[Either[RawError, A]] = {
      client
        .expect[A](postConstructor(uri, request, headers))
        .attempt
        .flatMap { response =>
          response
            .leftMap {
              case err: UnexpectedStatus =>
                handleErrors(err)
              case InvalidMessageBodyFailure(details, cause) =>
                RawDeserializationError(details, cause)
            }
            .pure[F]
        }
    }

    private def getConstructor(uri: Uri, headers: Headers) = {
      Request[F](
        method = Method.GET,
        uri = uri,
        headers = headers
      )
    }

    private def postConstructor[B: Encoder](
        uri: Uri,
        reqBody: B,
        headers: Headers
    ) = {
      Request[F](
        method = Method.POST,
        uri = uri,
        headers = headers
      ).withEntity(reqBody)
    }

    private def handleErrors(requestStatus: UnexpectedStatus): RawError =
      requestStatus.status match {
        case Status.GatewayTimeout =>
          RawTimeoutError("Timeout error")
        case Status.BadRequest =>
          RawBadRequestError("Bad Request")
        case status =>
          RawHttpError("Some HTTP error", status)
      }
  }
}
