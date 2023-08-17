package com.github.nikalaikina

import com.github.nikalaikina.{Interval, Order, Product}

import cats.effect.*
import cats.implicits.*
import fs2.Stream

import java.time.LocalDate.ofInstant
import java.time.ZoneOffset.*
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MONTHS
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID
import scala.concurrent.duration.*

@main
def main(): Unit = {
  println("Hello world!")
}

object service {
  import repo.*

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

