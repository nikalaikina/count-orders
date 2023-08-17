package com.github.nikalaikina

import fs2.Stream

trait ProductRepo[F[_]] {
  def find(ids: Set[ProductId]): Stream[F, Product]
}