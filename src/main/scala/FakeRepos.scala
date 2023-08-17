package com.github.nikalaikina
import cats.effect.IO
import org.scalacheck.Gen

import java.time.{Instant, LocalDate}

object FakeRepos {

  def create[F[_]]: (OrderRepo[F], ProductRepo[F]) = {
    val orders = Gen.listOfN(10000, Gens.orderGen).sample.get
    val products = orders
      .flatMap(_.items)
      .map(item =>
        item.product -> Gens.productGen.sample.get.copy(id = item.product)
      )
      .toMap

    val ordersRepo: OrderRepo[F] = { (_: LocalDate, _: LocalDate) =>
      fs2.Stream.emits(orders)
    }

    val productsRepo: ProductRepo[F] = { (ids: Set[ProductId]) =>
      fs2.Stream.emits(ids.toList.map(id => products(id)))
    }

    (ordersRepo, productsRepo)
  }

}
