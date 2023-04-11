int i = 3;
int j = 6;
void *t1()
{
  i = j + 1;
  i = j + 2;
}

void *t2()
{
  j = i + 1;
}

int transmitter_2()
{
  t1();
  t2();
  if (i >= 10 || j >= 11)
  {
    return 1;
  }
  else
  {
    return 0;
  }
}