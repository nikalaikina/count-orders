package com.github.nikalaikina

import fs2.Stream

import java.time.Instant

trait OrderRepo[F[_]] {
  def find(from: Instant, to: Instant): Stream[F, Order]
}
