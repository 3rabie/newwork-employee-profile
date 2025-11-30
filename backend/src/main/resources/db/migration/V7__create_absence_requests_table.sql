create table if not exists absence_requests (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references users(id) on delete cascade,
    manager_id uuid references users(id) on delete set null,
    start_date date not null,
    end_date date not null,
    type varchar(32) not null,
    status varchar(32) not null,
    note text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index if not exists idx_absence_requests_user on absence_requests(user_id);
create index if not exists idx_absence_requests_manager_status on absence_requests(manager_id, status);
