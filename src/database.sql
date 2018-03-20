 CREATE DATABASE wallet CHARACTER SET utf8 COLLATE utf8_bin;
 use wallet;
 create table user (id bigint auto_increment primary key,
 username varchar(128) unique, password varchar(1024), mobile varchar(20));

 create table user_wallet(id bigint auto_increment primary key, user_id bigint, account varchar(1024),
 private_key varchar(128), mnemonics varchar(1024));