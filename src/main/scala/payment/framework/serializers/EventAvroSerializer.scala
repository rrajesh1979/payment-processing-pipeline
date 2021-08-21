package payment.framework.serializers

import akka.serialization.Serializer
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema}
import org.apache.avro.Schema
import payment.framework.PaymentDomainModel.Event

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

class EventAvroSerializer extends Serializer {

  val eventSchema: Schema = AvroSchema[Event]

  override def identifier: Int = 75432

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case c: Event =>
      val baos = new ByteArrayOutputStream()
      val avroOutputStream = AvroOutputStream.binary[Event].to(baos).build(eventSchema)
      avroOutputStream.write(c)
      avroOutputStream.flush()
      avroOutputStream.close()
      baos.toByteArray
    case _ => throw new IllegalArgumentException("We only support Event & Payment for Avro")
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val inputStream = AvroInputStream.binary[Event].from(new ByteArrayInputStream(bytes)).build(eventSchema)
    val eventIterator: Iterator[Event] = inputStream.iterator
    val event = eventIterator.next()
    inputStream.close()

    event
  }

  override def includeManifest: Boolean = true
}