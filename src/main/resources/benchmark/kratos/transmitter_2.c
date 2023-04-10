int i = 3;
int j = 6;
void *t1()
{
  i = j + 1;
  i = j + 2;
  i = j + 3;
  i = j + 1;
  i = j + 1;
}

void *t2()
{
  j = i + 1;
  j = i + 1;
  j = i + 1;
  j = i + 1;
  j = i + 1;
}

int transmitter_2()
{
  int res;
  t1();
  t2();
  if (i >= 16 || j >= 16 || i <= 0 || j <= 0)
  {
    res = 0;
  }
  else
  {
    res = 1;
  }

  return (res);
}