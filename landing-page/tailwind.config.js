/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#4A90E2',
        'primary-dark': '#50C9C3',
        'primary-light': '#7ED4E6',
        'secondary': '#4A90E2',
        'secondary-dark': '#50C9C3',
        'secondary-light': '#7ED4E6',
      },
      fontFamily: {
        'chinese': ['PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'WenQuanYi Micro Hei', 'sans-serif'],
      },
      backgroundImage: {
        'gradient-primary': 'linear-gradient(45deg, #4A90E2, #7ED4E6, #50C9C3, #4A90E2, #7ED4E6, #50C9C3)',
        'gradient-secondary': 'linear-gradient(135deg, #4A90E2 0%, #7ED4E6 50%, #50C9C3 100%)',
      },
    },
  },
  plugins: [],
}
