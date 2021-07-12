package infrastructure.serialization.interpreter

import infrastructure.serialization.algebra.Deserializer.`failed to deserialize`
import infrastructure.serialization.algebra.{Deserializer, Serialization, Serializer}
import play.api.libs.json.{Format, JsValue, Json}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

abstract class `JSON Serialization`[A: ClassTag]
    extends Serializer[A]
    with Deserializer[A]
    with Serialization[A]
    with Deserializer.DeserializationValidator {
  implicit val json: Format[A]

  def example: A
  def exampleString = serialize(example, multiline = false)

  override def serialize(item: A, multiline: Boolean = true): String =
    multiline match {
      case true => Json.prettyPrint(json.writes(item))
      case false => Json.asciiStringify(json.writes(item))
    }

  override def validate(text: String): Either[`failed to deserialize`, Unit] =
    deserialize(text) match {
      case Left(value) => Left(value)
      case Right(value) => Right()
    }

  override def deserialize(text: String): Either[`failed to deserialize`, A] = {
    def ctag = implicitly[reflect.ClassTag[A]]
    def AClass: Class[A] = ctag.runtimeClass.asInstanceOf[Class[A]]
    if (AClass.getName == "java.lang.String") {
      Right(text.asInstanceOf[A])
    } else {
      Try {
        Json.parse(text).asOpt[A]
      } match {
        case Failure(invalidJson: com.fasterxml.jackson.core.JsonParseException) =>
          Left(
            `failed to deserialize`(
              s"""
                 | Failed to decode ${AClass.getName}
                 | because of:
                 | ${invalidJson.getMessage}
                 | message that failed was: 
                 | $text
                 | """.stripMargin
            )
          )
        case Success(None) =>
          Left(
            `failed to deserialize`(
              s"""
                 | Failed to decode ${AClass.getName}
                 | because of:
                 | ${Json.parse(text).validate}
                 | message that failed was: 
                 | $text
                 | """.stripMargin
            )
          )
        case Success(Some(done)) => Right(done)
      }
    }
  }

}
