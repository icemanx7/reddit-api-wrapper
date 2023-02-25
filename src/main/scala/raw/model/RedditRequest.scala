package raw.model

import io.circe.Codec
import io.circe.generic.semiauto._
object RedditRequest {
  final case class RedditRequest(someString: String)
  object RedditRequest {
    implicit val redditRequestCodec: Codec[RedditRequest] = deriveCodec
  }
}
