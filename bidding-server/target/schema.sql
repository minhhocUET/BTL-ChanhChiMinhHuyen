-- =====================================
-- DATABASE
-- =====================================
DROP DATABASE IF EXISTS auction_db;
CREATE DATABASE auction_db;

\c auction_db;

-- =====================================
-- USERS (User + Bidder + Seller)
-- =====================================
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password VARCHAR(100) NOT NULL,
                       role VARCHAR(10) NOT NULL, -- BIDDER / SELLER
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================
-- ITEMS (Item + Art + Electronics)
-- =====================================
CREATE TABLE items (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       starting_price NUMERIC(12,2) NOT NULL,
                       type VARCHAR(20) NOT NULL, -- ART / ELECTRONICS
                       seller_id INT NOT NULL,

                       FOREIGN KEY (seller_id) REFERENCES users(id)
                           ON DELETE CASCADE
);

-- =====================================
-- AUCTIONS
-- =====================================
CREATE TABLE auctions (
                          id SERIAL PRIMARY KEY,
                          item_id INT UNIQUE,
                          current_price NUMERIC(12,2),
                          start_time TIMESTAMP,
                          end_time TIMESTAMP,
                          status VARCHAR(20) DEFAULT 'OPEN',

                          FOREIGN KEY (item_id) REFERENCES items(id)
                              ON DELETE CASCADE
);

-- =====================================
-- BIDS
-- =====================================
CREATE TABLE bids (
                      id SERIAL PRIMARY KEY,
                      auction_id INT NOT NULL,
                      bidder_id INT NOT NULL,
                      amount NUMERIC(12,2) NOT NULL,
                      bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                      FOREIGN KEY (auction_id) REFERENCES auctions(id)
                          ON DELETE CASCADE,
                      FOREIGN KEY (bidder_id) REFERENCES users(id)
                          ON DELETE CASCADE
);