create table cause_frame
(
    id           uuid not null,
    class_loader text,
    module       text,
    module_ver   text,
    class_name   text not null,
    method       text not null,
    file         text,
    line         int,
    native       boolean default false,

    primary key (id)
);

create table cause
(
    id           uuid not null,
    cause_strand uuid,
    message      text,

    primary key (id),
    foreign key (cause_strand) references cause_strand (id)
);

create table cause_strand
(
    id         uuid not null,
    class_name text not null
);

create table fault_strand
(
    id uuid not null
);

create table fault
(
    id           uuid not null,
    fault_strand uuid,

    primary key (id),
    foreign key (fault_strand) references fault_strand (id)
);

create table fault_2_cause
(
    seq   int not null,
    fault uuid,
    cause uuid,

    foreign key (fault) references fault (id),
    foreign key (cause) references cause (id),
    unique (seq, fault, cause)
);

create table fault_strand_2_cause_strand
(
    seq          int not null,
    fault_strand uuid,
    cause_strand uuid,

    foreign key (fault_strand) references fault_strand (id),
    foreign key (cause_strand) references cause_strand (id),
    unique (seq, fault_strand, cause_strand)
);

create table cause_strand_2_cause_frame
(
    seq          int not null,
    cause_strand uuid,
    cause_frame  uuid,

    foreign key (cause_strand) references cause_strand (id),
    foreign key (cause_frame) references cause_frame (id),
    unique (seq, cause_strand, cause_frame)
);

create sequence eventSequence no maxvalue;

create table fault_event
(
    id           uuid,
    fault        uuid,
    fault_strand uuid,
    cause        uuid,
    cause_strand uuid,
    time         timestamp,
    seq          int not null,
    event_seq    int default nextval(eventSequence),

    primary key (id),
    foreign key (fault) references fault (id)
);
