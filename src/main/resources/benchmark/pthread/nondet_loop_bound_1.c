int x = 0;
int n = 20;
int check_x;
void *t1() {
    check_x = x;
}

void *t2() {
    int t;
    t = x;
    x = t + 1;
}

int nondet_loop_bound_1()
{
	t1();

	t2();
	t2();
	t2();
	t2();
	t2();

	t2();
	t2();
	t2();
	t2();
	t2();

	t2();
	t2();
	t2();
	t2();
	t2();

	t2();
	t2();
	t2();
	t2();
	t2();

	if (n >= 20 && n < 40)
	{
	    return 0;
	}
	else
	{
	    return 1;
	}
}

