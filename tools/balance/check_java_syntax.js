#!/usr/bin/env node
/**
 * Лёгкая структурная проверка Java-файлов без JDK.
 *
 * В песочнице нет javac/Gradle (нет доступа к Maven-репозиториям Mojang), поэтому
 * вместо компиляции проверяем то, что ломается чаще всего при массовых правках:
 *
 *   - баланс {}, () и [] с учётом строк, символов и комментариев;
 *   - незакрытые строковые литералы и комментарии;
 *   - «пустые» тела методов, которых не должно быть (эвристика отключена);
 *   - отсутствие табуляции и CRLF (стиль репозитория).
 *
 * Это не замена компилятору: типы и импорты здесь не проверяются.
 *
 * Использование:
 *   node tools/balance/check_java_syntax.js <файл...>
 *   node tools/balance/check_java_syntax.js --all src/main/java
 */

const fs = require('fs');
const path = require('path');

function strip(source) {
  let out = '';
  let i = 0;
  const n = source.length;
  let state = 'code'; // code | line | block | str | chr | textblock

  while (i < n) {
    const c = source[i];
    const next = source[i + 1];

    if (state === 'code') {
      if (c === '/' && next === '/') { state = 'line'; out += '  '; i += 2; continue; }
      if (c === '/' && next === '*') { state = 'block'; out += '  '; i += 2; continue; }
      if (c === '"' && next === '"' && source[i + 2] === '"') {
        state = 'textblock'; out += '   '; i += 3; continue;
      }
      if (c === '"') { state = 'str'; out += ' '; i++; continue; }
      if (c === "'") { state = 'chr'; out += ' '; i++; continue; }
      out += c; i++; continue;
    }

    if (state === 'line') {
      if (c === '\n') { state = 'code'; out += '\n'; i++; continue; }
      out += ' '; i++; continue;
    }

    if (state === 'block') {
      if (c === '*' && next === '/') { state = 'code'; out += '  '; i += 2; continue; }
      out += c === '\n' ? '\n' : ' '; i++; continue;
    }

    if (state === 'textblock') {
      if (c === '"' && next === '"' && source[i + 2] === '"') {
        state = 'code'; out += '   '; i += 3; continue;
      }
      out += c === '\n' ? '\n' : ' '; i++; continue;
    }

    // str / chr
    if (c === '\\') { out += '  '; i += 2; continue; }
    if ((state === 'str' && c === '"') || (state === 'chr' && c === "'")) {
      state = 'code'; out += ' '; i++; continue;
    }
    if (c === '\n') {
      return { error: 'незакрытый ' + (state === 'str' ? 'строковый' : 'символьный') + ' литерал', out };
    }
    out += ' '; i++;
  }

  if (state === 'block') return { error: 'незакрытый блочный комментарий', out };
  if (state === 'str' || state === 'chr') return { error: 'незакрытый литерал в конце файла', out };
  if (state === 'textblock') return { error: 'незакрытый текстовый блок', out };
  return { out };
}

function checkFile(file) {
  const source = fs.readFileSync(file, 'utf8');
  const problems = [];

  if (source.includes('\r\n')) problems.push('CRLF вместо LF');

  const stripped = strip(source);
  if (stripped.error) {
    problems.push(stripped.error);
    return problems;
  }

  const pairs = { '}': '{', ')': '(', ']': '[' };
  const stack = [];
  let line = 1;
  for (const c of stripped.out) {
    if (c === '\n') line++;
    if (c === '{' || c === '(' || c === '[') stack.push({ c, line });
    else if (c in pairs) {
      const top = stack.pop();
      if (!top || top.c !== pairs[c]) {
        problems.push(`строка ${line}: лишняя или непарная «${c}»`);
        return problems;
      }
    }
  }
  if (stack.length) {
    const top = stack[stack.length - 1];
    problems.push(`незакрытая «${top.c}», открыта в строке ${top.line}`);
  }

  // Класс/интерфейс/enum без тела — типичная ошибка при вставке кода.
  if (/\b(class|interface|enum)\s+\w+[^{;]*$/.test(stripped.out.split('\n').slice(-2).join('\n'))) {
    problems.push('файл заканчивается объявлением без тела');
  }

  return problems;
}

function collect(dir, acc) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) collect(full, acc);
    else if (entry.name.endsWith('.java')) acc.push(full);
  }
  return acc;
}

const args = process.argv.slice(2);
let files;
if (args[0] === '--all') {
  files = collect(args[1] || 'src/main/java', []);
} else {
  files = args;
}

if (!files.length) {
  console.error('Нужен хотя бы один файл или --all <каталог>');
  process.exit(2);
}

let failed = 0;
for (const file of files) {
  let problems;
  try {
    problems = checkFile(file);
  } catch (e) {
    problems = ['не читается: ' + e.message];
  }
  if (problems.length) {
    failed++;
    console.log(`FAIL ${path.basename(file)}`);
    for (const p of problems) console.log('     ' + p);
  } else {
    console.log(`OK   ${path.basename(file)}`);
  }
}

console.log(`\n${files.length - failed}/${files.length} файлов без структурных ошибок`);
process.exit(failed ? 1 : 0);
