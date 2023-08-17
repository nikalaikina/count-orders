package com.github.nikalaikina

import model.*
import repo.*
import service.OrdersReport

import cats.effect.unsafe.implicits.global
import cats.effect.{Concurrent, IO, Temporal}
import io.github.martinhh.derived.scalacheck.given
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties

import java.time.Instant

object ReportTest extends Properties("OrderReport") {

  property("all to one interval") = forAll {
    (
        orders: List[Order],
        product: Product,
        from: Instant,
        to: Instant,
        productCreationDate: Instant
    ) =>
      orders.nonEmpty ==> {
        val ordersFixed = orders.map(absQuantity)

        val report: IO[Map[Interval, BigInt]] =
          new OrdersReport[IO](
            ordersRepo(ordersFixed),
            productsRepo(product.copy(creationDate = productCreationDate))
          ).report(from, to)

        val map = report.unsafeRunSync()

        map.size == 1
      }
  }

  property("everything is counted") = forAll {
    (
        orders: List[Order],
        product: Product,
        from: Instant,
        to: Instant
    ) =>
      orders.nonEmpty ==> {
        val ordersFixed = orders.map(absQuantity)

        val report: IO[Map[Interval, BigInt]] =
          new OrdersReport[IO](
            ordersRepo(ordersFixed),
            productsRepo(product)
          ).report(from, to)

        val map = report.unsafeRunSync()

        val sum =
          ordersFixed.flatMap(_.items).map(_.quantity).map(BigInt.apply).sum

        map.values.sum == sum
      }
  }

  property("no orders") = forAll {
    (
        product: Product,
        from: Instant,
        to: Instant
    ) =>
      val report: IO[Map[Interval, BigInt]] =
        new OrdersReport[IO](
          ordersRepo(List.empty),
          productsRepo(product)
        ).report(from, to)

      val map = report.unsafeRunSync()
      map.isEmpty
  }

  def ordersRepo(orders: List[Order]): OrderRepo[IO] = {
    (_: Instant, _: Instant) =>
      fs2.Stream.emits(orders)
  }

  def productsRepo(product: Product): ProductRepo[IO] = {
    (ids: Set[ProductId]) =>
      fs2.Stream.emits(ids.toList.map(id => product.copy(id = id)))
  }

  private def absQuantity(o: Order): Order =
    o.copy(items = o.items.map(i => i.copy(quantity = math.abs(i.quantity))))
}
