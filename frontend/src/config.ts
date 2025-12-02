const toBoolean = (value: string | boolean | undefined): boolean => {
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'string') {
    return value.toLowerCase() === 'true';
  }
  return false;
};

export const featureFlags = {
  switchUser: toBoolean(import.meta.env.VITE_ENABLE_SWITCH_USER ?? 'true'),
};
