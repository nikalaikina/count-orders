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
        val ordersRepo: OrderRepo[IO] = new OrderRepo[IO] {
          override def find(
              from: Instant,
              to: Instant
          ): fs2.Stream[IO, Order] = {
            fs2.Stream.emits(orders.map(absQuantity))
          }
        }
        val productsRepo: ProductRepo[IO] = new ProductRepo[IO] {
          override def find(ids: Set[ProductId]): fs2.Stream[IO, Product] = {
            fs2.Stream.emits(
              ids.toList.map(id =>
                product.copy(id = id, creationDate = productCreationDate)
              )
            )
          }
        }

        val report: IO[Map[Interval, BigInt]] =
          new OrdersReport[IO](ordersRepo, productsRepo).report(from, to)

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

        val ordersRepo: OrderRepo[IO] = new OrderRepo[IO] {
          override def find(
              from: Instant,
              to: Instant
          ): fs2.Stream[IO, Order] = {
            fs2.Stream.emits(ordersFixed)
          }
        }
        val productsRepo: ProductRepo[IO] = new ProductRepo[IO] {
          override def find(ids: Set[ProductId]): fs2.Stream[IO, Product] = {
            fs2.Stream.emits(
              ids.toList.map(id => product.copy(id = id))
            )
          }
        }

        val report: IO[Map[Interval, BigInt]] =
          new OrdersReport[IO](ordersRepo, productsRepo).report(from, to)

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
        to: Instant,
        productCreationDate: Instant
    ) =>
      val ordersRepo: OrderRepo[IO] = new OrderRepo[IO] {
        override def find(
            from: Instant,
            to: Instant
        ): fs2.Stream[IO, Order] = {
          fs2.Stream.empty
        }
      }
      val productsRepo: ProductRepo[IO] = new ProductRepo[IO] {
        override def find(ids: Set[ProductId]): fs2.Stream[IO, Product] = {
          fs2.Stream.emits(
            ids.toList.map(id =>
              product.copy(id = id, creationDate = productCreationDate)
            )
          )
        }
      }

      val report: IO[Map[Interval, BigInt]] =
        new OrdersReport[IO](ordersRepo, productsRepo).report(from, to)

      val map = report.unsafeRunSync()
      map.isEmpty
  }

  private def absQuantity(o: Order): Order =
    o.copy(items = o.items.map(i => i.copy(quantity = math.abs(i.quantity))))
}
