package com.github.nikalaikina

import cats.effect.{Concurrent, IO, Temporal}
import com.github.nikalaikina.model.*
import com.github.nikalaikina.repo.*
import com.github.nikalaikina.service.OrdersReport
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import io.github.martinhh.derived.scalacheck.given
import cats.effect.unsafe.implicits.global
import java.time.Instant
import org.scalacheck.Prop.{forAll, propBoolean}

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


        println(map)
        map.size == 1
      }
  }

  private def absQuantity(o: Order): Order =
    o.copy(items = o.items.map(i => i.copy(quantity = math.abs(i.quantity))))
}
