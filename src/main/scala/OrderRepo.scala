package com.github.nikalaikina

import fs2.Stream

import java.time.{Instant, LocalDate}

trait OrderRepo[F[_]] {
  def find(from: LocalDate, to: LocalDate): Stream[F, Order]
}
