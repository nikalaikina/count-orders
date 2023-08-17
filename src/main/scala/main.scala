package com.github.nikalaikina

import cats.effect.kernel.Concurrent
import com.github.nikalaikina.model.*
import cats.implicits.*

import java.time.Instant
import java.util.UUID
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

@main
def main(): Unit = {
  println("Hello world!")
}

object service {
  import repo._

  class OrdersReport[F[_]: Concurrent](
      orders: OrderRepo[F],
      products: ProductRepo[F]
  ) {
    val Intervals: List[Interval] = ???
    val MaxProductRequest: Int = ???

    private def isInInterval(
        interval: Interval,
        date: Instant,
        now: Instant
    ): Boolean = ???

    def report(from: Instant, to: Instant): F[Map[Interval, Int]] = {

      val toInterval: Instant => Interval = ???

      val res: Stream[F, Map[Interval, Int]] = orders
        .find(from, to)
        .flatMap(order => Stream.emits(order.items))
        .foldMap(item =>
          Map(item.product -> item.quantity)
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

      res.compile.toList.map { (maps: List[Map[Interval, Int]]) =>
        maps.combineAll
      }
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

  case class Interval(
      startsAgo: FiniteDuration,
      endsAgo: FiniteDuration,
      show: String
  )

  opaque type OrderId = UUID
  opaque type CustomerId = UUID
  opaque type Category = String
  opaque type ProductId = UUID
  opaque type Amount = BigDecimal
  opaque type Weight = BigDecimal

  case class Order(
      id: OrderId,
      customer: CustomerId,
      total: Amount,
      placed: Instant,
      items: List[Item]
  )

//Item: information about the purchased item (cost, shipping fee, tax amount, ...)

  case class Item(
      product: ProductId,
      quantity: Int,
      cost: Amount,
      shippingFee: Amount,
      tax: Amount
  )

//Product: information about the product (name, category, weight, price, creation date, ...)

  case class Product(
      id: ProductId,
      name: String,
      category: Category,
      weight: Weight,
      price: Amount,
      creationDate: Instant
  )

}
