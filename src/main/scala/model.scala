package com.github.nikalaikina

import java.time.Instant
import java.util.UUID

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
