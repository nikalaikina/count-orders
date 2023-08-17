package com.github.nikalaikina

import cats.effect.{Concurrent, Temporal}
import cats.implicits.*
import fs2.Stream

import java.time.LocalDate.ofInstant
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MONTHS
import java.time.{Instant, LocalDate, ZoneOffset}

class OrdersReport[F[_]: Concurrent: Temporal](
    orders: OrderRepo[F],
    products: ProductRepo[F]
) {
  import OrdersReport._

  def report(from: LocalDate, to: LocalDate): F[Map[Interval, BigInt]] = {
    Temporal[F].realTimeInstant
      .map(ofInstant(_, UTC))
      .flatMap(today => report(today, from, to))
  }

  private def report(
      today: LocalDate,
      from: LocalDate,
      to: LocalDate
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

object OrdersReport {
  val Intervals: List[Interval] = List(
    Interval(endsMonthsAgo = 12, show = ">12 months"),
    Interval(endsMonthsAgo = 6, show = "7-12 months"),
    Interval(endsMonthsAgo = 3, show = "4-6 months"),
    Interval(endsMonthsAgo = 0, show = "1-3 months")
  )

  val MaxProductRequest: Int = 100
}
