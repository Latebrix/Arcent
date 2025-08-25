# Contributing to Arcent

Thanks for your interest in contributing to Arcent! Every contribution helps make the app better for everyone who wants to celebrate their wins.

## 🚀 Getting Started

### Prerequisites

Make sure you have these installed:

```bash
Android Studio (latest stable version)
JDK 17 or higher
Android SDK (API 24+)

```

### Setting Up Your Environment

After forking the repository to your own GitHub account:

```bash
# Clone your fork
git clone https://github.com/latebrix/Arcent.git

# Create a secrets.properties file (copy from example)
cp secrets.properties.example secrets.properties

```


## 🎯 Areas for Contribution

### High Priority
- **Statistics & Analytics** - Help users understand their achievement patterns
- **Notification System** - Gentle reminders to celebrate wins
- **Data Export** - Allow users to backup their achievements

### Medium Priority
- **Performance Improvements** - Make the app faster and smoother
- **Localization** - Translate the app to other languages
- **UI/UX Enhancements** - Make the app even more beautiful

### Documentation
- Improve code comments and documentation
- Add more examples and tutorials
- Update feature documentation

## 📝 Pull Request Process

1. **Create a Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make Your Changes**
   - Write clean, well-commented code
   - Follow existing code patterns
   - Add tests if applicable

3. **Test Your Changes**

4. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   ```

5. **Push and Create PR**
   ```bash
   git push origin feature/your-feature-name
   ```

## 🐛 Bug Reports

Found a bug? Please create an issue with:

- **Clear description** of the problem
- **Steps to reproduce** the issue
- **Expected vs actual behavior**
- **Device/Android version** info
- **Screenshots** if relevant

NOTE: Please enable the crash/bug reporting in settings so we can track it on SENTRY.

## 💡 Feature Requests

Have an idea for Arcent? We'd love to hear it! Please:

- Check if the feature already exists or is planned
- Describe the problem your feature would solve
- Explain how it fits with Arcent's mission
- Consider offering to help implement it

## 🔒 Security

If you discover a security vulnerability, please email the maintainer directly (info@arcent.tech) instead of creating a public issue.

## 📱 App Architecture

Arcent follows modern Android development practices:

- **MVVM Architecture** - Clean separation of concerns
- **Jetpack Compose** - Modern declarative UI
- **Repository Pattern** - Clean data layer abstraction
- **Dependency Injection** - Using Hilt for clean dependencies
- **Coroutines** - For asynchronous operations

## 🎨 Design Guidelines

- Keep animations smooth and purposeful
- Ensure accessibility best practices
- Focus on celebrating user achievements

## ❓ Questions?

- Check the existing issues and documentation first
- Create a new issue for questions
- Be respectful and patient

---

Thank you for helping make Arcent better! Every contribution, no matter how small, is a win worth celebrating. ✦
