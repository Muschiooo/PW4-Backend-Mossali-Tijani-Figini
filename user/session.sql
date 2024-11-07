create table user.session
(
    id         int auto_increment
        primary key,
    user_id    int                                 null,
    created_at timestamp default CURRENT_TIMESTAMP null,
    constraint session_user_id_fk
        foreign key (user_id) references user.user (id)
);

