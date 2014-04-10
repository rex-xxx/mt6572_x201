#ifdef ENABLE_MALLOC_FULL_BACKTRACE
extern "C" void *malloc_full_backtrace(size_t size);
inline void *
operator new (size_t size)
{
  return malloc_full_backtrace(size);
}

inline void *
operator new[] (size_t size)
{
  return malloc_full_backtrace(size);
}
#endif

#ifdef ENABLE_FREE_FULL_BACKTRACE
extern "C" void free_full_backtrace(void *mem);
inline void
operator delete (void *p)
{
  free_full_backtrace (p);
}
#endif