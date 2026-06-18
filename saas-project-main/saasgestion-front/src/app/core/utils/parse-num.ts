// Convertit un nombre copié/saisi quel que soit le format :
// "198735" / "198.735" / "198,735" (séparateur de milliers) ou
// "198735,50" / "198735.50" (séparateur décimal).
export function parseNum(s: string | null | undefined): number | null {
  if (!s) return null;
  let str = s.trim().replace(/\s/g, '').replace(/[€$]/g, '');
  if (!str) return null;

  const lastSep = Math.max(str.lastIndexOf(','), str.lastIndexOf('.'));

  if (lastSep === -1) {
    const n = parseFloat(str);
    return isNaN(n) ? null : n;
  }

  // Un séparateur suivi de 1 ou 2 chiffres en fin de chaîne = séparateur décimal.
  // Suivi de 3 chiffres = séparateur de milliers à retirer.
  const decimals = str.slice(lastSep + 1);
  const isDecimalSeparator = /^\d{1,2}$/.test(decimals);

  if (isDecimalSeparator) {
    const intPart = str.slice(0, lastSep).replace(/[.,]/g, '');
    str = intPart + '.' + decimals;
  } else {
    str = str.replace(/[.,]/g, '');
  }

  const n = parseFloat(str);
  return isNaN(n) ? null : n;
}
