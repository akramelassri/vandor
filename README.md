# Vandor

Vandor is a marketplace for handmade goods. Customers shop from independent sellers, sellers manage their listings and orders, and admins manage users and categories.

## Features

- Customer and seller registration, login, and logout.
- Product search, filters, guest carts, and checkout with one order per seller.
- Seller listings, photo uploads, stock management, and order fulfillment.
- Customer order history and cancellation.
- Admin user suspension and category management.

Checkout does not collect payments. The application uses the existing SQL schema without changing it.

## Technologies

Java 25, Jakarta Servlets, JSP/JSTL, Hibernate, PostgreSQL, Maven, and Tomcat 11. The frontend uses plain CSS and JavaScript. Tests use JUnit 5 and H2.

## Run locally

Install Java 25, Maven, and Docker with Compose, then run:

```sh
mvn package
docker compose up -d
```

Open [localhost:8080](http://localhost:8080). Compose starts PostgreSQL and Tomcat, initializes new databases from `db/init.sql`, and persists database data and uploaded photos.

You can also deploy `deploy/ROOT.war` to Tomcat 11 running Java 25 or newer. Set `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `UPLOAD_DIR` for your environment, and initialize PostgreSQL with `db/init.sql`.

### Optional demo data

With PostgreSQL running locally, set `SEED_PASSWORD` and run:

```sh
DB_URL=jdbc:postgresql://localhost:5432/vandor_db \
  java -cp 'target/ROOT/WEB-INF/classes:target/ROOT/WEB-INF/lib/*' com.vandor.Main seed
```

This adds sample products and accounts: `admin@vandor.test`, `buyer@vandor.test`, `maker@vandor.test`, and `maker2@vandor.test`. New accounts use `SEED_PASSWORD`; existing records are kept.

## Development

```sh
mvn test
./scripts/format-java.sh --check
```
