// Copyright (c) BARGHEST
// SPDX-License-Identifier: AGPL-3.0-or-later

package cmd

import (
	"context"
	"errors"
	"os"
	"os/exec"

	"github.com/BARGHEST-ngo/MESH/analyst/session"
	"github.com/BARGHEST-ngo/androidqf_mesh/adb"
	"github.com/peterbourgon/ff/v3/ffcli"
)

func AdbCmd(s *session.Session, in *Input) *ffcli.Command {
	return &ffcli.Command{
		Name:       "adb",
		ShortUsage: "adb <adb arguments>",
		ShortHelp:  "Run an adb command against a paired device, e.g. 'adb shell'",
		LongHelp: `Runs adb against a device paired with adbpair, without opening a second terminal.
The device is selected for you, so do not pass -s.

  adb shell              interactive shell, leave it with 'exit' or Ctrl-D
  adb shell pm list packages
  adb logcat             stop it with Ctrl-C`,
		Exec: func(ctx context.Context, args []string) error {
			return runAdb(ctx, args, s, in)
		},
	}
}

func runAdb(ctx context.Context, args []string, s *session.Session, in *Input) error {
	if len(args) == 0 {
		return errors.New("usage: adb <adb arguments>, e.g. 'adb shell'")
	}

	dev, err := chooseDevice(ctx, s, in)
	if err != nil {
		return err
	}
	addr := s.ADBAddr(dev.ID)
	if addr == "" {
		return errors.New("device not connected over ADB, run adbpair")
	}
	if err := initADB(); err != nil {
		return err
	}

	c := exec.Command(adb.Client.ExePath, append([]string{"-s", addr}, args...)...)
	c.Stdin, c.Stdout, c.Stderr = os.Stdin, os.Stdout, os.Stderr

	var exitErr *exec.ExitError
	if err := c.Run(); err != nil && !errors.As(err, &exitErr) {
		return err
	}
	return nil
}
