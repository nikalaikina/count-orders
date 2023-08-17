package com.github.nikalaikina

import cats.effect.*
import com.github.nikalaikina.model.*
import cats.implicits.*

import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID
import fs2.Stream

import java.time.LocalDate.ofInstant
import java.time.ZoneOffset.*
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MONTHS
import scala.concurrent.duration.*

@main
def main(): Unit = {
  println("Hello world!")
}

object service {
  import repo._

  class OrdersReport[F[_]: Concurrent: Temporal](
      orders: OrderRepo[F],
      products: ProductRepo[F]
  ) {
    val Intervals: List[Interval] = List(
      Interval(endsMonthsAgo = 12, show = ">12 months"),
      Interval(endsMonthsAgo = 6, show = "7-12 months"),
      Interval(endsMonthsAgo = 3, show = "4-6 months"),
      Interval(endsMonthsAgo = 0, show = "1-3 months")
    )

    val MaxProductRequest: Int = 100

    def report(from: Instant, to: Instant): F[Map[Interval, BigInt]] = {
      Temporal[F].realTimeInstant
        .map(ofInstant(_, UTC))
        .flatMap(today => report(today, from, to))
    }

    def report(
        today: LocalDate,
        from: Instant,
        to: Instant
    ): F[Map[Interval, BigInt]] = {
      val toInterval: Instant => Interval = { instant =>
        val date = ofInstant(instant, UTC)
        Intervals
          .find(i => today.minus(i.endsMonthsAgo, MONTHS) isAfter date)
          .getOrElse(Intervals.last)
      }

      val stream: Stream[F, Map[Interval, BigInt]] = orders
        .find(from, to)
        .flatMap(order => Stream.emits(order.items))
        .foldMap(item =>
          Map(item.product -> BigInt(item.quantity))
        ) // i assume products can fit in memory
        .flatMap(productsMap =>
          Stream.emits(productsMap.toList.grouped(MaxProductRequest).toSeq)
        )
        .flatMap(productsBatch =>
          val map = productsBatch.toMap
          products
            .find(map.keySet)
            .map(p => Map(toInterval(p.creationDate) -> map(p.id)))
        )
        .foldMonoid

      stream.compile.toList.map(_.combineAll)
    }
  }
}

object repo {
  trait OrderRepo[F[_]] {
    def find(from: Instant, to: Instant): Stream[F, Order]
  }
  trait ProductRepo[F[_]] {
    def find(ids: Set[ProductId]): Stream[F, Product]
  }
}

object model {

  // scalacheck gens doesnt seam to work for opaque types in scala 3
  type OrderId = UUID
  type CustomerId = UUID
  type Category = String
  type ProductId = UUID
  type Amount = BigDecimal
  type Weight = BigDecimal

  case class Interval(
      endsMonthsAgo: Int,
      show: String
  )

  case class Order(
      id: OrderId,
      customer: CustomerId,
      total: Amount,
      placed: Instant,
      items: List[Item]
  )

  case class Item(
      product: ProductId,
      quantity: Int,
      cost: Amount,
      shippingFee: Amount,
      tax: Amount
  )

  case class Product(
      id: ProductId,
      name: String,
      category: Category,
      weight: Weight,
      price: Amount,
      creationDate: Instant
  )

}
