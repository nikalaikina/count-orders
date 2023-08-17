package com.github.nikalaikina

import java.time.Instant
import java.util.UUID

opaque type OrderId = UUID
opaque type CustomerId = UUID
opaque type Category = String
opaque type ProductId = UUID
opaque type Amount = BigDecimal
opaque type Weight = BigDecimal

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
