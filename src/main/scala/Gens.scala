package com.github.nikalaikina

import java.time.Instant
import org.scalacheck.Prop.*
import org.scalacheck.{Arbitrary, Gen}

object Gens {

  // since 2017
  implicit val instantGen: Gen[Instant] =
    Gen.choose[Instant](Instant.ofEpochMilli(1480464000000L), Instant.now())

  given Arbitrary[Instant] = Arbitrary(instantGen)

  implicit val itemGen: Gen[Item] = for {
    product <- Gen.uuid
    quantity <- Gen.choose(1, 100)
    cost <- Gen.posNum[BigDecimal]
    shippingFee <- Gen.posNum[BigDecimal]
    tax <- Gen.posNum[BigDecimal]
  } yield Item(
    product = product,
    quantity = quantity,
    cost = cost,
    shippingFee = shippingFee,
    tax = tax
  )

  given Arbitrary[Item] = Arbitrary(itemGen)

  implicit val orderGen: Gen[Order] = for {
    id <- Gen.uuid
    customer <- Gen.uuid
    placed <- instantGen
    items <- Gen.nonEmptyListOf(itemGen)
  } yield Order(
    id = id,
    customer = customer,
    placed = placed,
    items = items
  )

  given Arbitrary[Order] = Arbitrary(orderGen)

  implicit val productGen: Gen[Product] = for {
    id <- Gen.uuid
    name <- Gen.alphaLowerStr
    category <- Gen.alphaLowerStr
    weight <- Gen.posNum[BigDecimal]
    price <- Gen.posNum[BigDecimal]
    creationDate <- instantGen
  } yield Product(
    id = id,
    name = name,
    category = category,
    weight = weight,
    price = price,
    creationDate = creationDate
  )

  given Arbitrary[Product] = Arbitrary(productGen)

}
