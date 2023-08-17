package com.github.nikalaikina

import com.github.nikalaikina.{Interval, Order, Product}

import cats.effect.*
import cats.implicits.*
import fs2.Stream

import java.time.LocalDate.ofInstant
import java.time.ZoneOffset.*
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MONTHS
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID
import scala.concurrent.duration.*

@main
def main(): Unit = {
  println("Hello world!")
}
