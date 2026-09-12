export const NURSE_PALETTE = [
  '#0d9488', // teal
  '#2563eb', // blue
  '#d97706', // amber
  '#db2777', // pink
  '#7c3aed', // violet
  '#16a34a', // green
  '#dc2626', // red
  '#0891b2', // cyan
  '#ca8a04', // yellow
  '#9333ea', // purple
]

export const UNASSIGNED_COLOR = '#9ca3af' // gray-400

export function colorForNurse(nurseId, orderedNurseIds) {
  if (!nurseId) return UNASSIGNED_COLOR
  const index = orderedNurseIds.indexOf(nurseId)
  return NURSE_PALETTE[index % NURSE_PALETTE.length]
}
