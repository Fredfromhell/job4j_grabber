create table post(
id serial primary key,
name text,
text text,
link text UNIQUE ,
created timestamp    
);