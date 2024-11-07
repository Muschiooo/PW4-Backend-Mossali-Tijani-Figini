create table user.user
(
    id                 int auto_increment
        primary key,
    name               varchar(255)                 null,
    email              varchar(255)                 not null,
    password           varchar(255)                 null,
    phone              varchar(255)                 null,
    role               enum ('client', 'admin')     not null,
    verification       enum ('pending', 'verified') not null,
    verification_token varchar(255)                 null,
    constraint email
        unique (email)
);

