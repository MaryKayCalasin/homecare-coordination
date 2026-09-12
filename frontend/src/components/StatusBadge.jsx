import { VISIT_STATUS_COLORS, VISIT_STATUS_LABELS } from '../utils/labels'

export function StatusBadge({ status }) {
  return (
    <span
      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${VISIT_STATUS_COLORS[status] ?? 'bg-gray-100 text-gray-700'}`}
    >
      {VISIT_STATUS_LABELS[status] ?? status}
    </span>
  )
}
