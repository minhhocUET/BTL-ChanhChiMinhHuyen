create table if not exists users
(
    id
    serial
    primary
    key,
    username
    varchar
(
    50
) not null
    unique,
    password varchar
(
    100
) not null,
    role varchar
(
    10
) not null,
    created_at timestamp default CURRENT_TIMESTAMP,
    balance numeric default 0
    );

alter table users
    owner to postgres;

create table if not exists items
(
    id
    serial
    primary
    key,
    name
    varchar
(
    100
) not null,
    description text,
    starting_price numeric
(
    12,
    2
) not null,
    type varchar
(
    20
) not null,
    seller_id integer not null
    references users
    on delete cascade
    );

create table if not exists auctions
(
    id
    serial
    primary
    key,
    item_id
    integer
    unique
    references
    items
    on
    delete
    cascade,
    current_price
    numeric
(
    12,
    2
),
    start_time timestamp,
    end_time timestamp,
    status varchar
(
    20
) default 'OPEN':: character varying
    );

create table if not exists bids
(
    id
    serial
    primary
    key,
    auction_id
    integer
    not
    null
    references
    auctions
    on
    delete
    cascade,
    bidder_id
    integer
    not
    null
    references
    users
    on
    delete
    cascade,
    amount
    numeric
(
    12,
    2
) not null,
    bid_time timestamp default CURRENT_TIMESTAMP
    );
