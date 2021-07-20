import infrastructure.transaction.algebra.KafkaTransaction
import org.reflections.Reflections

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.reflect.{classTag, ClassTag}

object FindAllTransactions extends App {

  getSubtypesOf[App]().foreach(println)

  println("---")
  def getSubtypesOf[C: ClassTag](
      packageNames: Set[String] = Set(
        "io.scalac.auction"
      )
  ): Set[Class[_]] = {
    def aux[C: ClassTag](packageName: String) = {
      val javaSet =
        new Reflections(packageName) // search for subclasses will be performed inside the 'model' package
          .getSubTypesOf(classTag[C].runtimeClass)
      javaSet.asScala.toSet
    }

    packageNames.flatMap { packageName =>
      aux[C](packageName)
    }
  }
}
