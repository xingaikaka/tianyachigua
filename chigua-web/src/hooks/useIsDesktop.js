import { useEffect, useRef, useState } from 'react';

/**
 * Responsive breakpoint helper. Returns true when viewport width is >= breakpoint (default 768px).
 * Debounces resize updates with requestAnimationFrame to avoid layout thrash.
 */
const useIsDesktop = (breakpoint = 768) => {
  const getIsDesktop = () => {
    if (typeof window === 'undefined') return true;
    return window.innerWidth >= breakpoint;
  };

  const [isDesktop, setIsDesktop] = useState(getIsDesktop);
  const rafRef = useRef(0);

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;

    const handleResize = () => {
      if (rafRef.current) {
        cancelAnimationFrame(rafRef.current);
      }
      rafRef.current = requestAnimationFrame(() => {
        rafRef.current = 0;
        setIsDesktop(getIsDesktop());
      });
    };

    window.addEventListener('resize', handleResize);

    return () => {
      if (rafRef.current) {
        cancelAnimationFrame(rafRef.current);
        rafRef.current = 0;
      }
      window.removeEventListener('resize', handleResize);
    };
  }, [breakpoint]);

  return isDesktop;
};

export default useIsDesktop;
