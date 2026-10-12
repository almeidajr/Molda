"""Exercise the installed pre-commit CLI in disposable Git repositories."""

import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


class HooksTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        shutil.copyfile(Path(__file__).resolve().parents[1] / '.pre-commit-config.yaml',
                        self.repo / '.pre-commit-config.yaml')
        self.command('git', 'init', '-q')
        self.command('git', 'config', 'core.autocrlf', 'false')

    def command(self, *args):
        return subprocess.run(args, cwd=self.repo, capture_output=True, text=True,
                              check=True).stdout

    def hook(self, name, filename, content, expected=0, stage='pre-commit'):
        path = self.repo / filename
        path.write_bytes(content)
        self.command('git', 'add', '--', filename)
        args = ['pre-commit', 'run', name, '--hook-stage', stage]
        args += (['--commit-msg-filename', filename] if stage == 'commit-msg'
                 else ['--files', filename])
        result = subprocess.run(args, cwd=self.repo, capture_output=True, text=True)
        self.assertEqual(result.returncode, expected, result.stdout + result.stderr)
        return path.read_bytes()

    def test_messages(self):
        accepted = [f'{kind}: update Android screen' for kind in
                    'feat fix refactor perf test docs build ci chore style revert'.split()]
        accepted += ['feat(ui)!: replace navigation', 'fix!: remove legacy theme',
                     'feat(New/Area): add screen', 'chore(deps): bump AndroidX',
                     'build(deps-dev): bump ktlint',
                     'feat: replace navigation\n\nBREAKING CHANGE: migrate saved routes']
        rejected = ['update screen', 'feature: add screen', 'feat:', 'feat: ',
                    'Merge branch main', 'fixup! feat: add screen',
                    'squash! fix: repair screen', 'amend! docs: update guide']
        for message, expected in [(m, 0) for m in accepted] + [(m, 1) for m in rejected]:
            with self.subTest(message=message):
                self.hook('conventional-pre-commit', 'message.txt',
                          (message + '\n').encode(), expected, 'commit-msg')

    def test_whitespace_and_line_endings(self):
        self.assertEqual(self.hook('trailing-whitespace', 'source.txt', b'text  \n', 1),
                         b'text\n')
        self.assertEqual(self.hook('end-of-file-fixer', 'source.txt', b'text', 1), b'text\n')
        self.assertEqual(self.hook('trailing-whitespace', 'guide.md', b'hard break  \n'),
                         b'hard break  \n')
        batch = b'@echo off  \r\necho hello\r\n'
        fixed = self.hook('trailing-whitespace', 'run.bat', batch, 1)
        self.assertEqual(fixed, b'@echo off\r\necho hello\r\n')
        self.assertEqual(self.hook('end-of-file-fixer', 'run.bat', fixed), fixed)

    def test_hygiene(self):
        self.hook('check-yaml', 'config.yaml', b'key: value\n')
        self.hook('check-yaml', 'config.yaml', b'key: [\n', 1)
        self.hook('check-merge-conflict', 'source.txt',
                  b'<<<<<<< HEAD\na\n=======\nb\n>>>>>>> branch\n', 1)
        # Synthetic marker only: no credential is generated or stored.
        self.hook('detect-private-key', 'key.txt',
                  b'-----BEGIN ' + b'RSA PRIVATE KEY-----\nfixture\n', 1)

    def test_added_file_boundary(self):
        self.hook('check-added-large-files', 'large.txt', b'x' * (500 * 1024))
        self.hook('check-added-large-files', 'large.txt', b'x' * (500 * 1024 + 1), 1)

    def test_ci_rejects_committed_large_file(self):
        (self.repo / 'large.txt').write_bytes(b'x' * (500 * 1024 + 1))
        self.command('git', 'add', 'large.txt')
        self.command('git', '-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.test',
                     'commit', '-qm', 'test: add size fixture')
        result = subprocess.run(
            ['pre-commit', 'run', 'check-all-large-files', '--hook-stage', 'manual', '--all-files'],
            cwd=self.repo, capture_output=True, text=True)
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn('large.txt', result.stdout)


if __name__ == '__main__':
    unittest.main()
