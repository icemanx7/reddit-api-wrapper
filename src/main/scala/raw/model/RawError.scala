package raw.model

import org.http4s.Status

sealed abstract class RawError(
    val message: String,
    maybeCause: Option[Throwable] = None
) extends RuntimeException(message, maybeCause.orNull)

object RawError {

  final case class RawDeserializationError(
      override val message: String,
      error: Option[Throwable]
  ) extends RawError(
        s"Failed to deserialise json with cause $message",
        error
      )

  final case class RawHttpError(
      override val message: String,
      statusCode: Status
  ) extends RawError(
        s"$message from service raw with statusCode: ${statusCode.code}"
      )

  final case class RawBadRequestError(someText: String)
      extends RawError(s"Bad request with cause: $someText")

  final case class RawTimeoutError(
      serviceName: String,
      error: Option[Throwable] = None
  ) extends RawError(s"Timeout in request to $serviceName", error)

}
