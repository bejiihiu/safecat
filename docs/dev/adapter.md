# Writing a SafeCat Adapter (User-Friendly)

- [Quick Start](#quick-start)
- [How It Works](#how-it-works)
- [Template](#template)
- [Base Classes](#base-classes)
- [Which Interfaces to Implement](#which-interfaces-to-implement)
- [Build & Deploy](#build--deploy)
- [Tips](#tips)

---

## Quick Start

**Ты — юзер, который хочет заставить свой мод дружить с SafeCat.**

1. Создай новый Gradle-проект (или открой существующий)
2. Добавь зависимость на SafeCat (`implementation "kz.bejiihiu:safecat-api:1.0.0"`)
3. Скопируй `extension-example/` к себе, переименуй package
4. Замени заглушки на реальные вызовы API твоего мода
5. Удали провайдеры, которые не нужны
6. Собери jar и кинь в `config/safecat/extensions/`

Готово. Больше ничего не надо.

---

## How It Works

```
твой мод-магазин → SafeCatAPI.withdraw() → твой адаптер → API экономического мода
```

Раньше, чтобы мод работал с SafeCat, нужно было:

1. Имплементить `CurrencyProvider` (3 метода)
2. Создавать `META-INF/services/kz.bejiihiu.safecat.api.CurrencyProvider`
3. Регистрироваться через ServiceLoader или события

Теперь достаточно:

```java
public class MyAdapter implements CurrencyProvider {
  public MyAdapter() {
    // одна строка — и готово
    SafeCatAPI.getInstance().registerAdapter(this);
  }
  // ... остальные методы
}
```

`registerAdapter()` сам смотрит, какие интерфейсы ты реализуешь (`CurrencyProvider`,
`PermissionProvider`, `ChatProvider`, `CommandProvider`), и регистрирует каждый.

### Почему это проще

| Было | Стало |
|------|-------|
| `META-INF/services/...` | одна строка в конструкторе |
| знать про ServiceLoader | знать только `registerAdapter(this)` |
| разбираться с init() | можно extend `BaseCurrencyAdapter` |
| отдельный файл на каждый провайдер | всё в одном классе |

---

## Template

Полный шаблон лежит в `extension-example/src/main/java/kz/bejiihiu/safecat/extension/example/ExampleExtension.java`.

Он показывает сразу все три типа:

- `CurrencyProvider` — экономика (баланс, транзакции)
- `PermissionProvider` — права доступа
- `ChatProvider` — префиксы/суффиксы/форматирование чата

Удали `implements` и методы тех типов, которые тебе не нужны.

```java
// например, только экономика:
public class MyAdapter implements CurrencyProvider {
  // ...
}

// или экономика + пермишены:
public class MyAdapter implements CurrencyProvider, PermissionProvider {
  // ...
}
```

---

## Base Classes

В `common/` лежат три базовых класса, которые сокращают шаблонный код:

### BaseCurrencyAdapter

```java
public class MyAdapter extends BaseCurrencyAdapter {
  public MyAdapter() {
    SafeCatAPI.getInstance().registerAdapter(this);
  }

  @Override
  public String getCurrencyId() { return "myeco:coin"; }

  @Override
  public CompletableFuture<BigDecimal> getBalance(UUID player) { ... }

  @Override
  public CompletableFuture<TransactionResult> withdraw(...) { ... }

  @Override
  public CompletableFuture<TransactionResult> deposit(...) { ... }
}
```

Что он делает за тебя:

| Метод | Что даёт |
|-------|----------|
| `init(registry)` | регистрирует Currency + провайдера |
| `priority()` | 0 (можно переопределить) |
| `supports(UUID)` | true (можно переопределить) |
| `getDisplayName()` | возвращает `getCurrencyId()` |
| `getSymbol()` | первый символ `getCurrencyId()` заглавной |
| `getCurrencyType()` | `CurrencyType.TOKEN` |

### BasePermissionAdapter

```java
public class MyPermsAdapter extends BasePermissionAdapter {
  public MyPermsAdapter() {
    SafeCatAPI.getInstance().registerAdapter(this);
  }

  @Override
  public CompletableFuture<Boolean> hasPermission(UUID player, String permission) { ... }
}
```

Что он делает:

| Метод | Что даёт |
|-------|----------|
| `init(registry)` | регистрирует провайдера |
| `getProviderId()` | имя класса (можно переопределить) |
| `hasPermission(p, perm, ctx)` | делегирует в `hasPermission(p, perm)` |

### BaseChatAdapter

Все методы уже имеют дефолты, возвращающие `Optional.empty()` или `message`.
Переопредели только то, что нужно.

---

## Which Interfaces to Implement

| Интерфейс | Когда нужен | Методов реализовать |
|-----------|-------------|---------------------|
| `CurrencyProvider` | есть экономический мод | 4 (balance, withdraw, deposit, currencyId) |
| `PermissionProvider` | есть мод с правами доступа | 2 (hasPermission x2) |
| `ChatProvider` | есть чат-мод с префиксами | 0-4 (что нужно, то и переопределяешь) |
| `CommandProvider` | хочешь добавить команду | 1 (execute) |

---

## Build & Deploy

### 1. build.gradle

```gradle
plugins {
    id 'java-library'
}

repositories {
    mavenCentral()
    // если SafeCat ещё не в maven central:
    maven { url "https://maven.bejiihiu.kz/releases" }
}

dependencies {
    implementation "kz.bejiihiu:safecat-api:1.0.0"
    // зависимость от твоего мода:
    implementation "curse.maven:some-economy-mod:1234567"
}
```

### 2. Сборка

```bash
./gradlew build
```

Готовый jar лежит в `build/libs/`.

### 3. Установка

Положи jar в папку `mods/` на сервере. SafeCat сам найдёт твой адаптер через
`registerAdapter()`.

---

## Tips

- **Не кешируй `SafeCatAPI.getInstance()`** в статическом поле — SafeCat может
  переинициализироваться при перезагрузке.

- **Используй `@Override` всегда** — компилятор поймает ошибку, если сигнатура
  метода не совпадает.

- **Возвращай `CompletableFuture` корректно.** Если твой API синхронный —
  оберни в `CompletableFuture.completedFuture()`. Если асинхронный —
  просто верни его future.

- **Валидация сумм теперь в ядре.** `SafeCatAPIImpl` сам проверяет `amount.signum() <= 0`
  до вызова провайдера. Можешь не дублировать, но хуже не будет.

- **Не бойся удалять лишнее.** Если нужна только экономика — удали `implements
  PermissionProvider` и `implements ChatProvider`. Файл станет в 2 раза короче.

- **Смотри на существующие адаптеры.** `provider-numismatic/` показывает
  полноценный CurrencyProvider. `example-chat/` и `example-permissions/` —
  примеры для чата и пермишенов.
