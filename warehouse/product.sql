create table warehouse.product
(
    id           int auto_increment
        primary key,
    name         varchar(255)                       not null,
    description  varchar(255)                       not null,
    price        int                                not null,
    stock        int                                not null,
    image        varchar(255)                       null,
    availability enum ('available', 'out of stock') not null
);

